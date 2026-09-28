import rclpy
from rclpy.node import Node
from std_msgs.msg import String
from example_interfaces.srv import AddTwoInts


class Listener(Node):
    def __init__(self):
        super().__init__('listener')
        self.sub = self.create_subscription(String, '/chatter', self.cb, 10)
        self.srv = self.create_service(AddTwoInts, '/add_two_ints', self.handle)

    def cb(self, msg):
        pass

    def handle(self, request, response):
        return response


def main():
    rclpy.init()
    node = Listener()
    rclpy.spin(node)


if __name__ == '__main__':
    main()
