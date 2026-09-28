import rclpy
from rclpy.node import Node
from std_msgs.msg import String


class Listener(Node):

    def __init__(self):
        super().__init__('listener')
        self.sub = self.create_subscription(String, 'chatter', self.cb, 10)  # @sub
        assert self.sub.topic == 'chatter'
        assert self.sub.topic_name == '/chatter'
        assert self.sub.raw == False

    def cb(self, msg):
        pass


def main():
    rclpy.init()
    Listener()


main()
