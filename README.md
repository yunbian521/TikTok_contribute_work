## 整个项目的功能是使用java来开发的，界面设计使用xml来实现，模拟抖音的“投稿”页面。
（演示视频百度网盘链接：https://pan.baidu.com/s/1zf1koIekE_t6PompSBjesA 提取码: 5t4q）  

项目中实现了以下的一些功能：  

### 投稿界面UI：实现了抖音投稿页面的简化版本，包含图片选择区和文案编辑区。  

    图文内容管理：长按图片实现了顺序调整，以及长按图片弹出删除toast(删除操作未能实现，需要更长时间的学习)，实现了文案的字数统计和字数限制。  
	
    文字编辑功能：实现了添加话题标签、热门话题选择以及 @ 用户功能。  
	
    位置信息服务：自动获取并显示当前设备的经纬度位置信息。  
	
### app中的一些java文件：这是一些逻辑代码。

	MainActivity.java：是App 的入口，和这个页面相关的所有操作（比如点击 “选图片” 按钮、显示经纬度、处理定位权限），都写在 MainActivity 里。  

	PreviewAdapter.java：是基于 RecyclerView.Adapter 实现的自定义适配器，将图片 Uri 列表与 RecyclerView 绑定，当预览列表的图片Uri 集合发生变化时，实现图片预览列表和封面的更新。  

	PhotoItemTouchHelperCallback.java：是自定义的基于 ItemTouchHelper.Callback 实现的回调类，为展示图片的RecyclerView 组件提供拖拽排序和滑动删除功能。  

	CandidateAdapter.java：是基于 RecyclerView.Adapter 实现的自定义适配器，核心作用是为 RecyclerView 提供 Mock用户列表的展示功能。  

	HotTopicAdapter.java：也是基于 RecyclerView.Adapter 实现的自定义适配器，核心作用是为 RecyclerView 提供话题标签列表的展示功能。  

	LocationUtil.java：是封装的有关定位的工具类，里面包含定位相关的逻辑函数，例如如定位权限检查、位置信息获取等。  

	Topic.java：实体类，包含话题标签的属性，例如名称、是否热门等。  

	User.java:也是实体类，包含用户ID、用户名等属性。  

### ui界面：  

	activity_main.xml文件是主用户界面。各种组件都在该文件中进行布局。

### res:  

	drawable：里面包含一些用到的布局和icon照片。  

	mipmap：里面是用来放各种像素的软件图标照片。  

	layout：是一些用于布局的xml文件。  

	values:里面是一些设置字符和、颜色的xml文件。

