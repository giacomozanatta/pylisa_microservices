import rclpy
from rclpy.node import Node
from example_interfaces.srv import AddTwoInts


class Server(Node):

    def __init__(self):
        super().__init__('server', namespace='robot1')
        self.srv = self.create_service(AddTwoInts, 'add', self.on_add)  # @srv
        assert self.srv.srv_name == 'add'
        assert self.srv.service_name == '/robot1/add'
        self.cli = self.create_client(AddTwoInts, 'add')  # @cli
        assert self.cli.srv_name == 'add'
        assert self.cli.service_name == '/robot1/add'
        req = AddTwoInts.Request(a=1, b=2)
        fut = self.cli.call_async(req)  # @call
        done = fut.done()  # @done

    def on_add(self, request, response):
        return response


def main():
    rclpy.init()
    Server()


main()
