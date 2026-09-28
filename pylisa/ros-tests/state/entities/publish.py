import rclpy
from rclpy.node import Node
from std_msgs.msg import String


class Talker(Node):

    def __init__(self):
        super().__init__('talker')
        self.pub = self.create_publisher(String, 'chatter', 10)
        msg = String()
        msg.data = 'hi'
        self.pub.publish(msg)  # @publish


def main():
    rclpy.init()
    Talker()


main()
