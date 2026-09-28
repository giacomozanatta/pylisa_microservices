import rclpy
from std_msgs.msg import String

rclpy.init()
node = rclpy.create_node('n')
pub = node.create_publisher(String, 'chatter', 10)
timer = node.create_timer(1.0, None)
assert node.count_publishers('/x') == 0  # @count
assert pub.get_subscription_count() == 0  # @subscribers
assert timer.is_ready()  # @ready
names = node.get_topic_names_and_types()
now = node.get_clock().now()
rate = node.create_rate(2)
node.get_logger().warn('w')
timer.cancel()
assert timer.is_canceled()  # @canceled
timer.reset()
assert not timer.is_canceled()  # @reset
destroyed = node.destroy_publisher(pub)  # @destroy
assert destroyed  # @destroyed
node.destroy_node()  # @dn
if input():
    node.get_name()
after = 1  # @after
