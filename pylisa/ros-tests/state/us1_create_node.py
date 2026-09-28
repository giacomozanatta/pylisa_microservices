import rclpy

rclpy.init()
node = rclpy.create_node('talker')  # @node
assert node.get_name() == 'talker'
assert node.get_namespace() == '/'
assert node.get_fully_qualified_name() == '/talker'
