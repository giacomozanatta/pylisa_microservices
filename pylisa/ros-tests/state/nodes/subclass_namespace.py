import rclpy
from rclpy.node import Node


class Talker(Node):

    def __init__(self):
        self.x = 3
        super().__init__('talker', namespace='robot1')  # @super
        assert self.x == 3
        assert self.get_name() == 'talker'
        assert self.get_namespace() == '/robot1'
        assert self.get_fully_qualified_name() == '/robot1/talker'


def main():
    rclpy.init()
    talker = Talker()
    rclpy.spin(talker)


if __name__ == '__main__':
    main()
