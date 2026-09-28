import rclpy

rclpy.init()
node = rclpy.create_node('n', namespace='robot1')
assert node.resolve_topic_name('chatter') == '/robot1/chatter'
assert node.resolve_topic_name('~/x') == '/robot1/n/x'
assert node.resolve_topic_name('/abs', only_expand=True) == '/abs'
assert node.resolve_service_name('add') == '/robot1/add'
