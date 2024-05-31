# home-center-pc-agent
家庭中心的PC端代理程序








----




#### 配置代理程序自启动
方法一：使用Windows任务计划程序

##### 创建批处理文件（.bat）：

打开记事本，输入以下内容：
```
@echo off
java -jar "C:\path\to\your\program.jar"

```

将文件保存为start_my_program.bat（确保路径和文件名正确）。

##### 创建任务计划：

- 打开“任务计划程序”（按Win + R，输入taskschd.msc）。
- 在右侧操作栏中选择“创建任务…”。
- 在“常规”选项卡中，为任务命名（例如“My Java Program”）。
- 在“触发器”选项卡中，点击“新建…”，选择“在登录时”作为触发条件。
- 在“操作”选项卡中，点击“新建…”，操作类型选择“启动程序”，在程序或脚本框中，浏览并选择刚才创建的start_my_program.bat文件。
- 点击“确定”保存任务。
- 