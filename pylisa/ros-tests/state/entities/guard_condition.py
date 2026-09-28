import rclpy
from rclpy.node import Node


class Waker(Node):

    def __init__(self):
        super().__init__('waker')
        self.g = self.create_guard_condition(self.wake)  # @g

    def wake(self):
        pass


def main():
    rclpy.init()
    Waker()


main()
