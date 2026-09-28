import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
topic = 'a' if input() else 'b'
pub = node.create_publisher(String, topic, 10)  # @pub
assert pub.topic_name == '/a'  # @neg
