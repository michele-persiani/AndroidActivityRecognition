# AndroidActivityRecognition



Android application that records and saves the data from the available sensor. Work in progress.

TODO: add Tensorflow Lite classifier

Main Components:
- **MainActivity**: the only purpose of the activity is to start the RecordService
- **RecordService**: main service that records and saves sensor data
  - **RecordServiceStarter**: starter for the RecordService
  - **RecurrentSave**: this component recurrently saves RecordServcie readings
  - **SensorAccumulatorManager**: manager handling the sensors and their accumulation
  - **Persistance**: class for saving on external files the current state of SensorAccumulatorManager



Author: **Michele Persiani**, michelep@cs.umu.se

**Copiright of Umea University**
