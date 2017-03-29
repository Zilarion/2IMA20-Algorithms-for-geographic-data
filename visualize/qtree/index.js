'use strict';
// Top left: [40,9, -74.25]
// Bottom right: [40.5, -73.7]


const select = document.getElementById("select");

window.initMap = function() {
    console.log(select.value);
    d3.select("#vis canvas").remove("*");
    d3.json(select.value + ".json", function (error, data) {
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
                .style("position", "absolute")
                .style("top", 0)
                .style("left", 0)
                .style("width", "100%")
                .style("height", "100%")
                .style("pointer-events", "none");

            const bounds = this.map.getBounds();
            const proj = this.getProjection();

            this.quadtree = {};
            let maxLevel = 0;
            let minLevel = 10000;
            data.forEach((d) => {
                if (!this.quadtree[d.depth]) {
                    this.quadtree[d.depth] = [];
                    if (d.depth > maxLevel) {
                        maxLevel = d.depth;
                    }
                    if (d.depth < minLevel) {
                        minLevel = d.depth;
                    }
                }
                this.quadtree[d.depth].push(d);
            });
            this.quadtree.minLevel = minLevel;
            this.quadtree.maxLevel = maxLevel;
            this.map.addListener('center_changed', this.onPan);
        };

        SVGOverlay.prototype.onPan = function () {
            if (this.panTimeout) {
                clearTimeout(this.panTimeout);
            }
            if (this.zoomTimeout) {
                clearTimeout(this.zoomTimeout);
            }
            this.panTimeout = timeoutDraw(this)
        };

        SVGOverlay.prototype.onRemove = function () {
            this.canvas.removeListener('center_changed', this.onPan);
            this.canvas.parentNode.removeChild(this.canvas);
            this.canvas = null;
        };

        SVGOverlay.prototype.draw = function () {
            if (this.panTimeout) {
                clearTimeout(this.panTimeout);
            }
            if (this.zoomTimeout) {
                clearTimeout(this.zoomTimeout);
            }
            this.zoomTimeout = timeoutDraw(this)
        };

        // Create the Google Map…
        const map = new google.maps.Map(d3.select("#map").node(), {
            zoom: 13,
            center: new google.maps.LatLng(40.75, -73.975),
            mapTypeId: google.maps.MapTypeId.TERRAIN
        });

        let overlay = new SVGOverlay(map);
    });
};

function timeoutDraw(ctx) {
    // Resize canvas to screen size
    ctx.canvas
        .attr("width", ctx.canvas.node().offsetWidth)
        .attr("height", ctx.canvas.node().offsetHeight);

    // Get projection and map bounds
    const proj = ctx.getProjection();
    const bounds = ctx.map.getBounds();
    const zoom = ctx.map.getZoom();

    // clear the canvas from previous drawing
    let cWidth = ctx.canvas.node().width;
    let cHeight = ctx.canvas.node().height;
    ctx.canvas.node().getContext('2d').clearRect(0, 0, cWidth, cHeight);

    return setTimeout(function() {
        paintCanvas(ctx.canvas, ctx.quadtree, proj, bounds, zoom);
    }, 50)
}

function paintCanvas(canvas, data, proj, bounds, zoom) {
    // get the canvas drawing context and the width and height
    const context = canvas.node().getContext('2d');
    let cWidth = canvas.node().width;
    let cHeight = canvas.node().height;

    // clear the canvas from previous drawing
    context.clearRect(0, 0, cWidth, cHeight);
    context.fillStyle = "rgba(0,0,200, 0.1)";
    context.fillRect(0, 0, cWidth, cHeight);

    let color = d3.scale.linear().domain([0,
        data.minLevel,
        (data.maxLevel - data.minLevel) / 2 + data.minLevel,
        data.maxLevel]).range(["blue","green", "yellow", "red"]);

    // Draw all rects
    for (const level in data) {
        if (data.hasOwnProperty(level)) {
            if (level != "maxLevel") {
                const quads = data[level];
                let cl = d3.rgb(color(level));
                context.fillStyle = "rgba(" + cl.r + "," + cl.g + "," + cl.b + "," + (Math.pow((level / data.maxLevel), 4)) + ")";
                context.beginPath();
                quads.forEach((d) => {
                    if (bounds.contains(new google.maps.LatLng(d.x1, d.y1)) ||
                        bounds.contains(new google.maps.LatLng(d.x2, d.y1)) ||
                        bounds.contains(new google.maps.LatLng(d.x1, d.y2)) ||
                        bounds.contains(new google.maps.LatLng(d.x2, d.y2))
                    ) {
                        const p1 = transformXY(proj, d.x1, d.y1);
                        const p2 = transformXY(proj, d.x2, d.y2);

                        context.rect(Math.ceil(p1.x), Math.ceil(p2.y), Math.ceil(p2.x - p1.x), Math.ceil(p1.y - p2.y));
                    }
                });
                context.fill();
                if (zoom > level) {
                    context.lineWidth = 0.1;
                    context.strokeStyle = "rgba(0,0,0,0.2)";
                    context.stroke();
                }
            }
        }
    }

}


function transformXY(proj, x, y) {
    return proj.fromLatLngToContainerPixel(new google.maps.LatLng(x, y));
}