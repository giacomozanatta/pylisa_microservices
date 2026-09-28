import rclpy
from rclpy.node import Node
from std_msgs.msg import String


class Talker(Node):

    def __init__(self):
        super().__init__('talker')
        self.pub = self.create_publisher(String, 'chatter', 10)  # @pub
        assert self.pub.topic == 'chatter'
        assert self.pub.topic_name == '/chatter'
        assert self.pub.qos_profile == 10


def main():
    rclpy.init()
    Talker()


main()
