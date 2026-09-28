# ROS 2 corpus summary (configuration CP, best-effort, no parameter values from outside)

| Program | Outcome | Nodes | Publishers | Subscriptions | Timers | Services | Clients | Unknown topics | rclpy exceptions | Imprecise constructs |
|---|---|---|---|---|---|---|---|---|---|---|
| action.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException |  |
| aware/dialog_node.py | OK | 1 | 2 | 3 | 0 | 8 | 5 | 0 |  | f-string, try |
| aware/director_node.py | OK | 1 | 1 | 0 | 0 | 7 | 0 | 0 |  | f-string |
| aware/psychologist_node.py | OK | 1 | 1 | 0 | 0 | 7 | 0 | 0 |  | f-string |
| aware/talk_node.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 |  | f-string |
| constant-prop.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| detect_ball.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  | f-string, try |
| detect_ball_3d.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| elephant_shooter/controller.py | EXCEPTION AnalysisException | | | | | | | | | f-string |
| elephant_shooter/mpc_node.py | EXCEPTION AnalysisException | | | | | | | | | f-string, try |
| elephant_shooter/mpc_node_open_loop.py | EXCEPTION AnalysisException | | | | | | | | | f-string |
| elephant_shooter/shooter_test.py | EXCEPTION IllegalStateException | | | | | | | | |  |
| fruit_collectors/collector_node.py | OK | 1 | 2 | 1 | 0 | 6 | 1 | 0 |  | f-string, try |
| fruit_collectors/vision_node.py | OK | 1 | 2 | 2 | 0 | 7 | 0 | 0 |  | f-string, try |
| init-procedure.py | OK | 1 | 2 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException |  |
| main.py | OK | 2 | 3 | 1 | 0 | 12 | 0 | 0 |  | %-format |
| main2.py | OK | 4 | 15 | 10 | 3 | 24 | 0 | 0 | InvalidParameterTypeException, NotInitializedException | %-format |
| main3.py | OK | 3 | 5 | 2 | 0 | 18 | 0 | 0 |  |  |
| minimal.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 | InvalidHandle, InvalidNodeNameException, InvalidParameterException, InvalidParameterTypeException, InvalidParameterValueException, ParameterAlreadyDeclaredException | %-format |
| pandas_2.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| qos.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| server.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 1 | InvalidHandle | async |
| simple_node/action.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException | lambda |
| simple_node/one_publisher.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException |  |
| simple_node/one_publisher_one_service_rclpy.py | OK | 1 | 2 | 0 | 0 | 7 | 1 | 0 | InvalidParameterTypeException, NotInitializedException | lambda |
| simple_node/one_publisher_rclpy.py | OK | 1 | 2 | 0 | 0 | 6 | 0 | 1 | RCLError, InvalidParameterTypeException, InvalidTopicNameException, NotInitializedException |  |
| test/intel_pub.py | EXCEPTION IOException | | | | | | | | | try |
| test/pasticcio01.py | OK | 1 | 3 | 0 | 0 | 0 | 0 | 0 |  | %-format |
| test/pasticcio02.py | EXCEPTION IllegalStateException | | | | | | | | | lambda |
| test/pasticcio03.py | OK | 1 | 2 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException |  |
| test/pasticcio04.py | OK | 1 | 2 | 1 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException |  |
| test/pasticcio05.py | OK | 1 | 2 | 0 | 1 | 6 | 0 | 0 |  | %-format |
| test/pointcloud_publisher.py | OK | 1 | 2 | 0 | 1 | 6 | 0 | 0 |  |  |
| test/subscription.py | OK | 2 | 3 | 1 | 0 | 12 | 0 | 0 |  | lambda |
| test_flow.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| test_publish/node01.py | OK | 1 | 2 | 0 | 0 | 6 | 1 | 0 | InvalidParameterTypeException, NotInitializedException | lambda |
| test_publish/node02.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException | lambda |
| test_publish/node03.py | OK | 1 | 1 | 0 | 0 | 6 | 0 | 0 | InvalidParameterTypeException, NotInitializedException | lambda |
| two-nodes/first_file.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| two-nodes/second_file.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
| typedargs/typed.py | OK | 0 | 0 | 0 | 0 | 0 | 0 | 0 |  |  |
