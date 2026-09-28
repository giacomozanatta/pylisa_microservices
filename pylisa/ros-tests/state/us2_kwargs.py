import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
pub = node.create_publisher(String, topic='chatter', qos_profile=10)  # @pub
assert pub.topic_name == '/chatter'
assert pub.qos_profile == 10
