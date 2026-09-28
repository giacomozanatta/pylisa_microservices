import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
pub = node.create_publisher(String, input(), 10)  # @pub
assert pub.topic_name == '/chatter'  # @neg
