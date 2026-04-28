const hotelbookingDb = db.getSiblingDB("hotelbooking");

const KAZAN_HOTEL_ID = "10000000-0000-0000-0000-000000000001";
const ALMETYEVSK_HOTEL_ID = "10000000-0000-0000-0000-000000000002";
const CHELNY_HOTEL_ID = "10000000-0000-0000-0000-000000000003";

const KAZAN_STANDARD_ID = "20000000-0000-0000-0000-000000000001";
const KAZAN_FAMILY_ID = "20000000-0000-0000-0000-000000000002";
const KAZAN_LUX_ID = "20000000-0000-0000-0000-000000000003";

const ALMETYEVSK_STANDARD_ID = "20000000-0000-0000-0000-000000000004";
const ALMETYEVSK_BUSINESS_ID = "20000000-0000-0000-0000-000000000005";

const CHELNY_ECONOMY_ID = "20000000-0000-0000-0000-000000000006";
const CHELNY_STANDARD_ID = "20000000-0000-0000-0000-000000000007";

const AVAILABILITY_FROM = "2030-06-01";
const AVAILABILITY_TO = "2030-06-30";

const hotels = [
  {
    _id: KAZAN_HOTEL_ID,
    name: "Demo Kazan Hotel",
    city: "Kazan",
    roomTypes: [
      {
        roomTypeId: KAZAN_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      },
      {
        roomTypeId: KAZAN_FAMILY_ID,
        name: "FAMILY",
        guestCapacity: 4
      },
      {
        roomTypeId: KAZAN_LUX_ID,
        name: "LUX",
        guestCapacity: 4
      }
    ]
  },
  {
    _id: ALMETYEVSK_HOTEL_ID,
    name: "Demo Almetyevsk Hotel",
    city: "Almetyevsk",
    roomTypes: [
      {
        roomTypeId: ALMETYEVSK_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      },
      {
        roomTypeId: ALMETYEVSK_BUSINESS_ID,
        name: "BUSINESS",
        guestCapacity: 2
      }
    ]
  },
  {
    _id: CHELNY_HOTEL_ID,
    name: "Demo Naberezhnye Chelny Hotel",
    city: "Naberezhnye Chelny",
    roomTypes: [
      {
        roomTypeId: CHELNY_ECONOMY_ID,
        name: "ECONOMY",
        guestCapacity: 1
      },
      {
        roomTypeId: CHELNY_STANDARD_ID,
        name: "STANDARD",
        guestCapacity: 2
      }
    ]
  }
];

const availabilityDefinitions = [
  {
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_STANDARD_ID,
    totalRooms: 6
  },
  {
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_FAMILY_ID,
    totalRooms: 3
  },
  {
    hotelId: KAZAN_HOTEL_ID,
    roomTypeId: KAZAN_LUX_ID,
    totalRooms: 2
  },
  {
    hotelId: ALMETYEVSK_HOTEL_ID,
    roomTypeId: ALMETYEVSK_STANDARD_ID,
    totalRooms: 4
  },
  {
    hotelId: ALMETYEVSK_HOTEL_ID,
    roomTypeId: ALMETYEVSK_BUSINESS_ID,
    totalRooms: 2
  },
  {
    hotelId: CHELNY_HOTEL_ID,
    roomTypeId: CHELNY_ECONOMY_ID,
    totalRooms: 5
  },
  {
    hotelId: CHELNY_HOTEL_ID,
    roomTypeId: CHELNY_STANDARD_ID,
    totalRooms: 5
  }
];

function parseDateOnly(dateString) {
  return new Date(dateString + "T00:00:00.000Z");
}

function toDateOnlyString(date) {
  return date.toISOString().substring(0, 10);
}

function upsertHotels() {
  for (const hotel of hotels) {
    hotelbookingDb.hotels.replaceOne(
      { _id: hotel._id },
      hotel,
      { upsert: true }
    );
  }
}

function upsertAvailability() {
  const from = parseDateOnly(AVAILABILITY_FROM);
  const to = parseDateOnly(AVAILABILITY_TO);

  for (const definition of availabilityDefinitions) {
    for (
      let current = new Date(from);
      current <= to;
      current.setUTCDate(current.getUTCDate() + 1)
    ) {
      const date = new Date(current);
      const dateString = toDateOnlyString(date);

      const availabilityId =
        definition.hotelId + ":" + definition.roomTypeId + ":" + dateString;

      const availability = {
        _id: availabilityId,
        hotelId: definition.hotelId,
        roomTypeId: definition.roomTypeId,
        date: date,
        totalRooms: definition.totalRooms,
        heldRooms: 0,
        bookedRooms: 0
      };

      hotelbookingDb.room_availability.replaceOne(
        { _id: availabilityId },
        availability,
        { upsert: true }
      );
    }
  }
}

function clearDemoHolds() {
  hotelbookingDb.room_holds.deleteMany({
    hotelId: {
      $in: [KAZAN_HOTEL_ID, ALMETYEVSK_HOTEL_ID, CHELNY_HOTEL_ID]
    }
  });
}

function printSummary() {
  print("Demo inventory data has been initialized.");
  print("Availability range: " + AVAILABILITY_FROM + " .. " + AVAILABILITY_TO);
  print("Hotels:");

  const hotelSummaries = hotelbookingDb.hotels
    .find(
      {
        _id: {
          $in: [KAZAN_HOTEL_ID, ALMETYEVSK_HOTEL_ID, CHELNY_HOTEL_ID]
        }
      },
      {
        name: 1,
        city: 1,
        roomTypes: 1
      }
    )
    .toArray();

  printjson(hotelSummaries);
}

upsertHotels();
upsertAvailability();
clearDemoHolds();
printSummary();