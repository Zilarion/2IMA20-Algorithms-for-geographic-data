'use strict';
// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]

window.initMap = function() {
    d3.json("qtree.json", function (error, data) {
        if (error) throw error;
        var google = window.google;
        function SVGOverlay (map) {
            this.map = map;
            this.canvas = null;
            this.quadtree = null;

            this.onPan = this.onPan.bind(this);

            this.setMap(map);
        }

        SVGOverlay.prototype = new google.maps.OverlayView();

        SVGOverlay.prototype.onAdd = function () {
            let base = d3.select("#vis");
            this.canvas = base.append("canvas")
                .attr("width", "1920")
                .attr("height", "1080")
                .style("position", "absolute")
                .style("top", 0)
                .style("left", 0)
                .style("width", "100%")
                .style("height", "100%")
                .style("pointer-events", "none");

            const bounds = this.map.getBounds();
            const proj = this.getProjection();
            this.quadtree = data;

            paintCanvas(this.canvas, this.quadtree, proj, bounds);

            this.onPan();
            this.map.addListener('center_changed', this.onPan);
        };

        SVGOverlay.prototype.onPan = function () {
            const proj = this.getProjection();
            const bounds = this.map.getBounds();
            paintCanvas(this.canvas, this.quadtree, proj, bounds);
        };

        SVGOverlay.prototype.onRemove = function () {
            this.canvas.removeListener('center_changed', this.onPan);
            this.canvas.parentNode.removeChild(this.canvas);
            this.canvas = null;
        };

        SVGOverlay.prototype.draw = function () {
            const proj = this.getProjection();
            const bounds = this.map.getBounds();
            paintCanvas(this.canvas, this.quadtree, proj, bounds);
        };

        // Create the Google Map…
        const map = new google.maps.Map(d3.select("#map").node(), {
            zoom: 11,
            center: new google.maps.LatLng(40.7, -73.975),
            mapTypeId: google.maps.MapTypeId.TERRAIN
        });

        let overlay = new SVGOverlay(map);
    });
};

function paintCanvas(canvas, data, proj, bounds) {
    // Resize canvas to screen size
    canvas
        .attr("width", canvas.node().offsetWidth)
        .attr("height", canvas.node().offsetHeight);

    // get the canvas drawing context and the width and height
    const context = canvas.node().getContext('2d');
    let cWidth = canvas.node().width;
    let cHeight = canvas.node().height;

    // clear the canvas from previous drawing
    context.clearRect(0, 0, cWidth, cHeight);

    // Draw all rects
    data.forEach((d) => {
        // if (bounds.contains(transformXY(proj, d.x1, d.y1)) ||
        //     bounds.contains(transformXY(proj, d.x2, d.y1)) ||
        //     bounds.contains(transformXY(proj, d.x1, d.y2)) ||
        //     bounds.contains(transformXY(proj, d.x2, d.y2))) {
        //     return;
        // }
        if (d.depth < 8) { return ;}
        const p1 = transformXY(proj, d.x1, d.y1);
        const p2 = transformXY(proj, d.x2, d.y2);

        // console.log(p1.x, p2.y, (p2.x - p1.x), (p1.y-p2.y));
        context.beginPath();
        context.rect(p1.x, p2.y, p2.x - p1.x, p1.y - p2.y);
        context.fillStyle = "rgba(255, 0, 0, 0.1)";
        context.fill();
        context.closePath();
    });
}


function transformXY(proj, x, y) {
    return proj.fromLatLngToContainerPixel(new google.maps.LatLng(x, y));
}