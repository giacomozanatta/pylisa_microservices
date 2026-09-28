import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n', namespace='robot1')
pub = node.create_publisher(String, '/chatter', 10)  # @pub
assert pub.topic == '/chatter'
assert pub.topic_name == '/chatter'
