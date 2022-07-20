# MagMapBuild
A mag map build app, which is suitable for my "MagMapAndPosition" project.
Using the mark point function, the app will record the corrective points while the app recording the sensors data for PDR.

## data columns
All data are stored in two csv file. The structure of the data shown below:

### ..._sensors.csv 
Use for PDR and mag.
|字段|位置|说明|
|----|---|----|
|time|0|时间戳|
|TYPE_ACCELEROMETER|1 2 3|加速度xyz|
|TYPE_GYROSCOPE|4 5 6|陀螺仪xyz|
|TYPE_MAGNETIC_FIELD|7 8 9|磁力计xyz|
|TYPE_GAME_ROTATION_VECTOR|10 11 12 13|无磁方向四元数xyzw|

### ..._points.csv 
Use for PDR correction (as Ground Truth).
|字段|位置|说明|
|----|---|----|
|time|0|时间戳|
|float X|1|标记点横坐标|
|float Y|2|标记点纵坐标|
