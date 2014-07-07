# wikj

A tiny wiki.

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein ring server

Make pages persist after a restart with the WIKJ_BACKUP_FILE environment variable:

    WIKJ_BACKUP_FILE=/foo/bar/wikj.sexp lein ring server

## License

Copyright © 2014 R.W. van 't Veer
