import rclpy
from rclpy.node import Node


class Talker(Node):

    def __init__(self):
        super().__init__('talker')
        self.t = self.create_timer(0.5, self.tick)  # @t
        assert self.t.timer_period_ns == 500000000
        fast = self.create_timer(0.1, self.tick)
        assert fast.timer_period_ns == 100000000
        slow = self.create_timer(5, None)
        assert slow.timer_period_ns == 5000000000
        assert slow.callback is None

    def tick(self):
        pass


def main():
    rclpy.init()
    Talker()


main()
