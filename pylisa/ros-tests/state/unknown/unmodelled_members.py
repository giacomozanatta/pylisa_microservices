import rclpy
from rclpy.action import ActionServer
from rclpy.lifecycle import LifecycleNode

rclpy.init()
node = rclpy.create_node('n')
server = ActionServer(node, None, 'fib', None)
node.not_an_api()
assert node.get_name() == 'n'  # @name
