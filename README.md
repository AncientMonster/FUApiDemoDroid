# FUApiDemoDroid
Faceunity Android SDK API使用示例，详细的集成文档请参考[这里](https://github.com/Faceunity/FUQiniuDemoDroid/blob/master/README.md)。
##示例介绍
本Demo展示了独立使用FU SDK API的用法。FU SDK和任何三方无耦合，FU SDK负责范围本质上只是接收输入的图像，输出处理后的图像,如果有对接三方SDK如推流的需求，可以参考本Demo对FU SDK API的使用。FU SDK不涉及视频编码及网络，使用者可以自由选择。
##示例项目构成
###FUDualInputToTextureExampleActivity
这个Activity演示了从Camera取数据,用fuDualInputToTexure处理得到绘制道具结果并使用GLSurfaceView预览展示。所谓DualInput，是指从cpu和gpu同时拿数据源，cpu拿到的是nv21的byte数组，gpu拿到的是对应的texture，具体请参考对应代码。
###FURendererToNV21ImageExampleActivity
这个Activity演示了如何通过fuRenderToNV21Image，在无GL Context的情况下输入nv21的人脸图像，输出添加道具及美颜后的nv21图像。这个Activity只演示API的使用，无预览效果，FU SDK使用者可以将拿到处理后的nv21图像与自己的原有项目对接。请FU SDK使用者直接参考示例放至代码至对应位置。
##问题反馈
有疑问或者bug反馈，可以在项目中提issue。
