'use strict';

// Create the Google Map…
const map = new google.maps.Map(d3.select("#map").node(), {
    zoom: 11,
    center: new google.maps.LatLng(40.7, -73.975),
    mapTypeId: google.maps.MapTypeId.TERRAIN
});


// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]
d3.json("results.json", function(error, data) {
    if (error) throw error;

    const value = data["1451602800000"];

    const overlay = new google.maps.OverlayView();
    overlay.draw = function () {

        d3.selectAll(".gridOverlay").remove();
        const layer = d3.select(this.getPanes().overlayLayer).append("div")
            .attr("class", "gridOverlay");

        const projection = this.getProjection();
        const gridSize = 50;
        const deltaLat = (40.5 - 40.9) / gridSize;
        const deltaLon = (-73.7 - -74.25) / gridSize;

        let grid = [];
        for (let i = 0; i < gridSize; i++) {
            for (let j = 0; j < gridSize; j++) {
                grid.push({lat: 40.9 + deltaLat * i, lon: -74.25 + deltaLon * j, value: value[j + i * gridSize]});
            }
        }

        layer.selectAll(".border")
            .data([{lat: 40.9, lon: -74.25}])
            .each(transformBorder)
            .enter()
            .append("rect")
            .attr({
                "class": "border",
                "fill": "none",
                "shape-rendering": "crispEdges",
                "stroke": "red",
                "stroke-width": "4px"
            }).each(transformBorder);

        const tile = layer.selectAll("svg")
            .data(grid)

        tile.enter()
            .append("svg")
            .each(transformTile)
            .attr("class", "grid");

        tile.append("rect")
            .attr(
                {
                    "class": "tile",
                    "fill": "none",
                    "shape-rendering": "crispEdges",
                    "stroke": "black",
                    "stroke-width": "1px"
                })
            .each(transformTile);

        function transformBorder(d) {
            let topLeft = new google.maps.LatLng(d.lat, d.lon);
            let bottomRight = new google.maps.LatLng(d.lat + deltaLat * gridSize, d.lon + deltaLon * gridSize);

            topLeft = projection.fromLatLngToDivPixel(topLeft);
            bottomRight = projection.fromLatLngToDivPixel(bottomRight);

            // console.log(topLeft, bottomRight);
            return d3.select(this)
                .style("left", (topLeft.x) + "px")
                .style("top", (topLeft.y) + "px")
                .style("width", (topLeft.x - bottomRight.x) + "px")
                .style("height", (topLeft.y - bottomRight.y) + "px")
        }

        function transformTile(d) {
            let color = d3.scale.pow().domain([0,1000]).range(["green", "red"]);
            let topLeft = new google.maps.LatLng(d.lat, d.lon);
            let bottomRight = new google.maps.LatLng(d.lat + deltaLat, d.lon + deltaLon);

            topLeft = projection.fromLatLngToDivPixel(topLeft);
            bottomRight = projection.fromLatLngToDivPixel(bottomRight);

            const text = d3.select(this)
                .append("text")
                .text(d.value)
                .style("fill", "white")
                .style("position", "relative")
                // .style("font-size", "70%")
                .attr({
                    "x": "50%",
                    "y": "50%",
                    "alignment-baseline": "middle",
                    "text-anchor": "middle"
                });

            return d3.select(this)
                .style("left", (topLeft.x) + "px")
                .style("top", (topLeft.y) + "px")
                .style("width", (bottomRight.x - topLeft.x) + "px")
                .style("height", (bottomRight.y - topLeft.y) + "px")
                .style("opacity", 0.5)
                .style("background-color", color(d.value));
        }
    };
    overlay.setMap(map);
})