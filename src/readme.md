## 注意事项
    1. 一定要把Opencv的路径添到ldconfig中
        # cat /etc/ld.so.conf
        include ld.so.conf.d/*.conf
        # echo "/usr/local/lib" >> /etc/ld.so.conf
        # ldconfig
    2.对于课题五的yolo 需要改 cfg中的coconame目录
    3.shuffling0  lost 
        加大内参
    4.stream_properties中不能加“”
    5.要保证 写回的kafka一定存在
## 需要进行的修改
    1. 对于配置文件 需要修改成从某目录中读取
## 需要完善的bug
### 1YOLO相关
TID 411, 115.157.201.215, executor 1):
java.lang.NegativeArraySizeException	at
Detector.computeBoxesAndAccByInputBytes(Native Method) at
Detector.execComputeBoxesAndAccByInputBytes(Detector.ja va:41)	at
Detector.startYolo(Detector.java:111) at
ImageProcessor2.processTrack(ImageProcessor2.java:161) at
ReadPhoto$2.call(ReadPhoto.java:93) at
ReadPhoto$2.call(ReadPhoto.java:89) at
org.apache.spark.sql.KeyValueGroupedDataset$$anonfun$fl
### 2Spark内存相关
20/06/19 03:52:27 INFO TaskSetManager: Task 51.1 in stage 7.0 (TID 831)
failed, but the task will not be re -executed (either because the task
failed with a shuffle data fetch failure, so the previous stage needs to
be re-run, or because a different copy of the task has already
succeeded).20/06/19 03:52:27 INFO TaskSchedulerImpl: Removed TaskSet
7.0, whose tasks h ave all completed, from pool 20/06/19 03:52:27 INFO
DAGScheduler: ResultStage 7 (start at ReadPhoto.java:124) failed in
33.554 s due to org.apache.spark.shuffle.MetadataFetchFailedException:
Missing an output location for shuffle 3	at
### 3. 关于HyperLPR
    编译：
        需要更改cmakeFile中的OPENCV_DIR 
        需要更改jni的头文件    
 ## 4.关于运行yolo出错
 java.lang.NegativeArraySizeException at
 Detector.computeBoxesAndAccByInputBytes(Native Method) at
 Detector.execComputeBoxesAndAccByInputBytes(Detector.java:41) at
 Detector.startYolo(Detector.java:111) at #  
 (1) yolo文件的设置必须设置正确，否则段错误
 "/home/user/Apache/App1/config/yolo/data/coco.names"
 ### 5. 合并两个过程测试，不然就是分开的运行过程，无法两种查询的实现并行
 1 合并两个流程到一个算子中  
 2.使用公平调度模式
