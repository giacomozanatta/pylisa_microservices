import rclpy
from std_msgs.msg import String

rclpy.init()
if input():
    rclpy.create_node('1bad')
after_node = 1  # @node
if input():
    rclpy.create_node('ok', namespace='/a/')
after_namespace = 1  # @namespace
node = rclpy.create_node('n')
if input():
    node.create_publisher(String, 'a//b', 10)
after_topic = 1  # @topic
