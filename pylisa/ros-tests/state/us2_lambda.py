import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
sub = node.create_subscription(String, 'chatter', lambda msg: None, 10, raw=True)  # @sub
assert sub.topic_name == '/chatter'
assert sub.raw == True
