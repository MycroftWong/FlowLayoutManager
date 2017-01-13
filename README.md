# 一个FlowLayoutManager库

修改了其他库的一些bug, 实现自己需要的```FlowLayoutManager```

## 使用方法

project的build.gradle 中

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

在项目的build.gradle中加上依赖

```gradle
dependencies {
    compile 'com.github.MycroftWong:FlowLayoutManager:v1.0'
}
```

具体实现，请参考[LayoutManager分析与实践](LayoutManager分析与实践.md)