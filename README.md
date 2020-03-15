
# 简单流式布局的实现

## 自定义view的绘制流程

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314201819.png)

## 自定义view流程和显示的对比

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314203109.png)

自定义view就像在一个空的房子里面装修一样
- 首先要反复测量房子的长宽高对应view的onMeasure
- 然后根据测量的结果进行空间布置划分房间,对应着viewGroup的onLayout
- 最后在划分的不同的房间内进行装修,对应view的onDraw

自定义view:
- 自定义view主要实现onMeasure + onDraw
- 自定义viewGroup主要实现onMeasure + onLayout

比如viewgroup很大 所以要测量在布局每个子view
对比房间很大进行不同隔断和房间的划分一样

比如空间很小就一间小屋子就不需要在布置了,直接测量根据测量的参数进行装修

## View的树形层级关系

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314204231.png)

当测量的时候从顶部DecorView内部执行measure方法内部调用了onMeasure,要想得到当前view的大小,首先要知道子view的大小,子view如果是viewgroup,那继续遍历子view的大小,最后一个view返回自己大小,然后viewgroup根据view反馈的大小计算出自己的大小并返回给自己的上一级,如此递归调用

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314204947.png)

## 自定义viewGroup时序图

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314205045.png)


## MeasureSpec是什么

MeasureSpc是View的内部类,内部进行位运算返回一个int值,由于int是32位,用最高2位表示mode,低30位表示size,MODE_SHIFT = 30 作用是移位

低30位已经表示很大的值了,基本没有那么大的size

- UNSPECIFIED:不对view大小做限制 最高两位:00
- EXACTLY:确切的大小,如:100dp  最高两位:01
- AT_MOST:大小最多是多少,不超过某个值,如match_parent最大不能超过父viewGroup的大小  最高两位:10

```java
 public static int makeMeasureSpec(@IntRange(from = 0, to = (1 << MeasureSpec.MODE_SHIFT) - 1) int size,
                                  @MeasureSpecMode int mode) {
    if (sUseBrokenMakeMeasureSpec) {
        return size + mode;
    } else {
        return (size & ~MODE_MASK) | (mode & MODE_MASK);
    }
}
```

## 计算子View的MeasureSpec

在自定义viewGroup的时候,在测量onMeasure中先测量子View,也就是调用子View的measure方法,这个方法要传入父ViewGroup给子View的MeasureSpec,通过getChildMeasureSpec(),计算


通过getChildMeasureSpec()方法实现的功能如下:

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314210749.png)

```java
public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
```

- 第一个参数传入父ViewGroup的MeasureSpec
- 第二个参数传入父ViewGroup的padding,计算子View的MeasureSpec要去除fuViewGroup的padding
- 第三个参数传入子View的宽高(通过子View.getLayoutParams()获取到LayoutParams,在获取width/height)

## LayoutParams

代表View的`layout_width`和`layout_height`的值

最后最重要的就是getChildMeasureSpec中计算得到子view的MeasureSpec,然后调用子View的measure()测量

## 布局需要了解的坐标系

onLayout的时候就只需要针对测量数据,进行每个子View的布局layout方法就可以了

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314221228.png)


## getMeasureWidth与getWidth的区别

1. getMeasureWidth
- 在measure()过程结束后就可以获取到对应的值
- 通过setMeasureDimension()方法进行设置的


2. getWidth
- 在layout()过程结束后才能获取到zhi
- 通过视图右边的坐标减去左边的坐标计算出来的

![](https://raw.githubusercontent.com/lqxue/picture_list/master/image/20200314223148.png)

## 自定义ViewGroup 流失布局

FlowLayout,流式布局,这个概念在移动端或者前端开发中很常见,特别是在多标签的展示中,往往起到了关键的作用。然而Android官方,并没有为开发者提供这样个布局

### 先测量子View

```java
@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    //测量子view

    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();

    //获取子View的个数
    int childCount = getChildCount();
    //遍历子View进行测量
    for (int i = 0; i < childCount; i++) {
        View childView = getChildAt(i);
        LayoutParams childAtLayoutParams = childView.getLayoutParams();

        //获取子View的MeasureSpec
        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, childAtLayoutParams.width);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom, childAtLayoutParams.height);

        //叫子View测量
        childView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    //测量自己

}
```

### 在测量自己

根据测量子view后的宽高,结合自己的场景计算当前view的模式和宽高,最后设定当前view的宽高

### 布局子view

当测量之后,这个测量方法不止调用一次,经过多次的测量后得到了每个view的具体的宽高,然后在onLayout中进行布局,根据场景计算出每个view的left/top/right/Bottom,调用子view的layout,此处不需要调用流式布局的layout,因为每个view都布置好了,那流失布局作为父viewGroup的布置也就结束了,类似于装修,先测量,拿到测量后的数据布置每个房间,最后房子也就是流式布局就装修好了,至于每个view怎样测量怎样绘制,那就要看是使用的自定义view还是系统view了

