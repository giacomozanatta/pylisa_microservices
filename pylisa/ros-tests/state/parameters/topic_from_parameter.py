import rclpy
from rclpy.node import Node
from rclpy.parameter import Parameter
from std_msgs.msg import String


class Talker(Node):

    def __init__(self):
        super().__init__('n')
        self.declare_parameter('topic', 'chatter')
        t = self.get_parameter('topic').value  # @value
        self.pub = self.create_publisher(String, t, 10)  # @pub
        assert self.has_parameter('topic')
        assert not self.has_parameter('missing')
        assert self.get_parameter_or('missing', Parameter('missing', value=3)).value == 3
        maybe = self.get_parameter(input())  # @undeclared


def main():
    rclpy.init()
    Talker()


main()
