import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('talker')
short = node.create_publisher(String, '~a', 10)
assert short.topic_name == '/talkera'
tiny = node.create_timer(-1e-10, None)
assert tiny.timer_period_ns == 0
name = input()
resolved = node.resolve_topic_name(name)  # @resolve
qos = None if input() else 10
pub = node.create_publisher(String, 'q', qos)  # @qos
period = float(input())
timer = node.create_timer(period, None)  # @timer
braces = node.create_publisher(String, '{x}/y', 10)  # @braces
