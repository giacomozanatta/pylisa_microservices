import rclpy
from rclpy.executors import SingleThreadedExecutor

rclpy.init()
n = rclpy.create_node('n')
m = rclpy.create_node('m')
ex = SingleThreadedExecutor()
ex.add_node(n)  # @add
rclpy.spin_once(m, timeout_sec=0.1)  # @spin
