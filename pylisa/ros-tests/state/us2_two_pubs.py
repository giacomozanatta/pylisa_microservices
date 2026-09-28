import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
a = node.create_publisher(String, 'a', 10)  # @p1
b = node.create_publisher(String, 'b', 5)  # @p2
assert a.topic_name == '/a'
assert b.topic_name == '/b'
