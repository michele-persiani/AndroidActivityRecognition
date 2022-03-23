# AndroidActivityRecognition



Android application that records and saves the data from the available sensors.


Main Components:
- **MainActivity**: the only purpose of the activity is to start the RecordService. By removing the finish() line it shows the sensor readings from the device.
- **RecordService**: main service that records and saves sensor data
  - **SensorAccumulatorManager**: manager handling the sensors and their accumulation into dataframes
  - **RecordServiceStarter**: starter for the RecordService. Used to restart the service if the system shuts it down.
  - **Persistance**: class for saving on external files the current state of SensorAccumulatorManager
  - **RecurrentSave**: this component recurrently saves RecordService readings

  - **TODO**: add Tensorflow Lite classifier


Author: **Michele Persiani**, michelep@cs.umu.se

**Copiright of Umea University**
