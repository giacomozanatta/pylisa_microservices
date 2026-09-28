from std_msgs.msg import String, Header
from example_interfaces.srv import AddTwoInts
import nowhere_msgs.msg

m = String()
assert m.data == ''  # @default
m.data = 'hi'
assert m.data == 'hi'  # @assigned
given = String(data='x')
assert given.data == 'x'  # @keyword

r = AddTwoInts.Request(a=1)
assert r.a == 1  # @request_given
assert r.b == 0  # @request_default

h = Header()
assert h.frame_id == ''  # @nested_owner
assert h.stamp.sec == 0  # @nested_default

unknown = nowhere_msgs.msg.Thing()  # @unknown
