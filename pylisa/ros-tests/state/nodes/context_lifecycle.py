import rclpy

rclpy.init()
assert rclpy.ok()
rclpy.shutdown()
assert not rclpy.ok()
if input():
    rclpy.create_node('late')
after = 1  # @late
