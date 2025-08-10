import geocoder

def get_coordinates_by_geocoder():
    g = geocoder.ip('me')
    return (g.lat, g.lng)

longitude = get_coordinates_by_geocoder()[1]
latitude = get_coordinates_by_geocoder()[0]
print("latitude:"+str(latitude))
print("longitude:"+str(longitude))
#print(get_coordinates_by_geocoder())
