import rclpy
from std_msgs.msg import String


class App:

    def __init__(self):
        self.node = rclpy.create_node('app')
        self.pub = self.node.create_publisher(String, 'out', 10)  # @pub
        assert self.pub.topic_name == '/out'


def main():
    rclpy.init()
    App()


main()
