# delay-task-schedule
简单实现延时任务starter
此功能需依赖redis做任务缓存以及延迟任务排序
延迟任务实现主要依赖于redis zset来做任务排序，通过拿取zset头部元素判断是否应该调用用户设定的回调方法
# 集成步骤：
（由于jar包没有上传到maven仓库所以需要自己将jar部署到本地仓库）
<p>1、下载项目的release代码自己打jar包</p>
<p>2、使用maven命令将jar部署到本地仓库，命令如下：mvn install:install-file -Dfile="jar包所在绝对路径" -Dpackaging=jar -DgroupId="com.yush" -DartifactId="delay-task-schedule" -Dversion="1.0.0"</p>
<p>3、在自己项目pom文件中引入</p>

![image](https://user-images.githubusercontent.com/42856806/198820883-551b68c5-b914-4be3-b02a-d78370afda46.png)
<p>4、编写回调方法，注意方法需加回调注解并标明方法名，如下：</p>

![image](https://user-images.githubusercontent.com/42856806/198820413-2c7a5eee-52ec-4c8b-8230-0724048bf72c.png)

<p>其中callbackHandle即是回调方法名，也是在我们提交延迟任务时需传入的方法名参数值</p>
<p>5、提交和终止延迟任务，如下：</p>
首先注入DelayTaskService
<p>然后通过DelayTaskService来提交任务</p>

![image](https://user-images.githubusercontent.com/42856806/198820729-85ff558e-7446-4b96-9444-6b9dba575707.png)
