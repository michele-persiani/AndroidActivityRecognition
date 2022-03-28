# AndroidActivityRecognition



Android application that records and saves the data from the available sensors.


Main Components:
- **MainActivity**: for testing purposes the activity has buttons to utilize the main components of the application. The final version will remove the activity and keep only the functionalities from RecordService.
- **ActivityWatchFace**: watchface to be utilized on a watch. The watchface shows the current results of the activity classifier. TODO: We will connect the watchface with the Tensorflow classifiers once they're ready.
- **RecordService**: main service that records and saves sensor data
  - **SensorAccumulatorManager**: manager handling the sensors and their accumulation into dataframes
  - **RecordServiceStarter**: starter for the RecordService. Used to restart the service if the system shuts it down
  - **Persistance**: class for saving on external files the current state of SensorAccumulatorManager
  - **RecurrentSave**: this component recurrently saves RecordService readings
  - **TensorflowLiteModels**: Singleton tensorflow models that uses sensors


Author: **Michele Persiani**, michelep@cs.umu.se

**Copiright of Umea University**.
