import rclpy
from rclpy.parameter import Parameter

rclpy.init()
node = rclpy.create_node('n')
assert node.has_parameter('use_sim_time')
if input():
    node.declare_parameter('use_sim_time', True)
after_sim = 1  # @sim


def decl(n, k):
    n.declare_parameter(k, 0)


decl(node, 'a')
decl(node, 'b')
got = node.get_parameter('a')
has_b = node.has_parameter('b')  # @summary
node.declare_parameter('x')
assert node.get_parameter_or('x', Parameter('x', value=3)).value == 3
node.declare_parameter('rate', 10)
if input():
    typed = node.declare_parameter('typed', 10)
after_typed = 1  # @typed
local = rclpy.create_node('local', cli_args=['--ros-args', '-p', 'p:=5'])
p = local.declare_parameter('p', 0).value  # @cli
node.add_on_set_parameters_callback(lambda params: None)
node.declare_parameter('checked', 1)  # @callback
rclpy.spin_once(node, timeout_sec=0.0)
r = node.get_parameter('rate').value  # @spun
