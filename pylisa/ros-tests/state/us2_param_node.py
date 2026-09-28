import rclpy
from rclpy.parameter import Parameter

rclpy.init()
given = rclpy.create_node('given', parameter_overrides=[Parameter('topic', value='other')])
t = given.declare_parameter('topic', 'chatter').value  # @given
loose = rclpy.create_node('loose', allow_undeclared_parameters=True)
assert loose.get_parameter('x').value is None
changed = rclpy.create_node('changed')
changed.declare_parameter('rate', 10)
changed.set_parameters([Parameter('rate', value=20)])
r = changed.get_parameter('rate').value  # @changed
changed.declare_parameter('rate', 5)  # @again
