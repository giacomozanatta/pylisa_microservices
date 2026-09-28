import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n', namespace='/r')
status = node.create_publisher(String, '~/status', 10)
assert status.topic_name == '/r/n/status'
own = node.create_publisher(String, '~', 10)
assert own.topic_name == '/r/n'
root = rclpy.create_node('n')
root_status = root.create_publisher(String, '~/status', 10)
assert root_status.topic_name == '/n/status'
