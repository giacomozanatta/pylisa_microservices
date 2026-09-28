import rclpy
from std_msgs.msg import String, Header

rclpy.init()
node = rclpy.create_node('n')
pub = node.create_publisher(String, 'chatter', 10)
if input():
    pub.publish(Header())
after_other = 1  # @other
if input():
    pub.publish(3)
after_int = 1  # @int
pub.publish(String())  # @right
