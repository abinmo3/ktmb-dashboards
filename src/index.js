export default {
  async fetch(request) {
    // CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, {
        headers: {
          "Access-Control-Allow-Origin": "*",
          "Access-Control-Allow-Methods": "GET,OPTIONS",
          "Access-Control-Allow-Headers": "Content-Type",
          "Access-Control-Max-Age": "86400",
        },
      });
    }

    // Upstream GTFS-Realtime feed (protobuf)
    const upstream = "https://api.data.gov.my/gtfs-realtime/vehicle-position/ktmb";
    const res = await fetch(upstream);

    const headers = new Headers(res.headers);
    headers.set("Access-Control-Allow-Origin", "*");
    headers.set("Cache-Control", "no-store");

    // Ensure content type for protobuf
    if (!headers.get("Content-Type")) {
      headers.set("Content-Type", "application/x-protobuf");
    }

    return new Response(res.body, { status: res.status, headers });
  },
};

