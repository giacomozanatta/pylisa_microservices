import rclpy
from rclpy.node import Node
from std_msgs.msg import String


class Talker(Node):
    def __init__(self):
        super().__init__('talker')
        self.pub = self.create_publisher(String, '/chatter', 10)
        self.cli = self.create_client(String, '/spawn_robot')


def main():
    rclpy.init()
    node = Talker()
    rclpy.spin(node)


if __name__ == '__main__':
    main()
