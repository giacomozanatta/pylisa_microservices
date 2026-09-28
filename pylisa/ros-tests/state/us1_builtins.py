import rclpy

rclpy.init()
n = rclpy.create_node('n')  # @node
m = rclpy.create_node('m', start_parameter_services=False)  # @node2
