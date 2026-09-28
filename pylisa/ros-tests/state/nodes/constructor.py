import rclpy
from rclpy.node import Node

rclpy.init()
n = Node('talker', namespace='/robot1')  # @node
assert n.get_name() == 'talker'
assert n.get_namespace() == '/robot1'
assert n.get_fully_qualified_name() == '/robot1/talker'
